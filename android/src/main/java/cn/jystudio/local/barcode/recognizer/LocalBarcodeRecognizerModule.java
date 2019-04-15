package cn.jystudio.local.barcode.recognizer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Base64;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class LocalBarcodeRecognizerModule extends ReactContextBaseJavaModule {
  private static final String BARCODE_CODE_TYPE_KEY = "codeTypes";

  private static final Map<String, Object> VALID_BARCODE_TYPES =
      Collections.unmodifiableMap(new HashMap<String, Object>() {
        {
          put("aztec", BarcodeFormat.AZTEC.toString());
          put("ean13", BarcodeFormat.EAN_13.toString());
          put("ean8", BarcodeFormat.EAN_8.toString());
          put("qr", BarcodeFormat.QR_CODE.toString());
          put("pdf417", BarcodeFormat.PDF_417.toString());
          put("upc_e", BarcodeFormat.UPC_E.toString());
          put("datamatrix", BarcodeFormat.DATA_MATRIX.toString());
          put("code39", BarcodeFormat.CODE_39.toString());
          put("code93", BarcodeFormat.CODE_93.toString());
          put("interleaved2of5", BarcodeFormat.ITF.toString());
          put("codabar", BarcodeFormat.CODABAR.toString());
          put("code128", BarcodeFormat.CODE_128.toString());
          put("maxicode", BarcodeFormat.MAXICODE.toString());
          put("rss14", BarcodeFormat.RSS_14.toString());
          put("rssexpanded", BarcodeFormat.RSS_EXPANDED.toString());
          put("upc_a", BarcodeFormat.UPC_A.toString());
          put("upc_ean", BarcodeFormat.UPC_EAN_EXTENSION.toString());
        }
      });

  LocalBarcodeRecognizerModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  /**
   * @return the name of this module. This will be the name used to {@code require()} this module
   * from javascript.
   */
  @Nonnull @Override
  public String getName() {
    return "LocalBarcodeRecognizer";
  }

  @ReactMethod
  public void decode(String base64Data, ReadableMap options, final Promise p) {
    try {
      RecognitionData data = new RecognitionData(base64Data, options, p);

      new RecognitionTask().execute(data);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  /**
   * Pojo class containing data for Recognition task
   */
  private class RecognitionData {
    private String base64Data;

    private ReadableMap options;

    private Promise promise;

    private String decodedBarcode;

    RecognitionData(String base64DataValue, ReadableMap optionsValue, Promise promiseValue) {
      base64Data = base64DataValue;
      options = optionsValue;
      promise = promiseValue;
    }

    String getBase64Data() {
      return base64Data;
    }

    ReadableMap getOptions() {
      return options;
    }

    Promise getPromise() {
      return promise;
    }

    String getDecodedBarcode() {
      return decodedBarcode;
    }

    void setDecodedBarcode(String decodedBarcodeValue) {
      decodedBarcode = decodedBarcodeValue;
    }
  }

  /**
   * Recognition task
   */
  private static class RecognitionTask extends AsyncTask<RecognitionData, RecognitionData, RecognitionData> {
    @Override protected RecognitionData doInBackground(RecognitionData... recognitionDataValue) {
      RecognitionData recognitionData = recognitionDataValue[0];

      String result = RecognitionUtils.decodeBarCode(recognitionData.getBase64Data(), recognitionData.getOptions());

      recognitionData.setDecodedBarcode(result);

      return recognitionData;
    }

    @Override protected void onPostExecute(RecognitionData recognitionDataValue) {
      super.onPostExecute(recognitionDataValue);

      recognitionDataValue.getPromise().resolve(recognitionDataValue.getDecodedBarcode());
    }
  }

  /**
   * Helper methods for Recognition task
   */
  private static class RecognitionUtils {
    static String decodeBarCode(String base64Data, ReadableMap options) {
      byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
      Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

      MultiFormatReader reader = new MultiFormatReader();

      if (options.hasKey(BARCODE_CODE_TYPE_KEY)) {
        ReadableArray codeTypes = options.getArray(BARCODE_CODE_TYPE_KEY);

        assert codeTypes != null;

        if (codeTypes.size() > 0) {
          EnumMap<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
          EnumSet<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
          for (int i = 0; i < codeTypes.size(); i++) {
            String code = codeTypes.getString(i);
            String formatString = (String) VALID_BARCODE_TYPES.get(code);
            if (formatString != null) {
              decodeFormats.add(BarcodeFormat.valueOf(formatString));
            }
          }
          hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
          reader.setHints(hints);
        }
      }

      int rotationAngleStep = 90;

      for (int angle = 0; angle != 360; angle += rotationAngleStep) {
        try {
          BinaryBitmap bitmap = generateBitmapFromImageData(rotateImage(decodedByte, angle));
          Result result = reader.decode(bitmap);

          if (!result.getText().equals("")) {
            return result.getText();
          }
        } catch (NotFoundException eValue) {
          eValue.printStackTrace();
        }
      }

      return "";
    }

    static BinaryBitmap generateBitmapFromImageData(Bitmap bitmap) {
      int[] mImageData = new int[bitmap.getWidth() * bitmap.getHeight()];

      bitmap.getPixels(mImageData, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
      LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), mImageData);

      return new BinaryBitmap(new HybridBinarizer(source));
    }

    static Bitmap rotateImage(Bitmap src, float degree) {
      // create new matrix
      Matrix matrix = new Matrix();

      // setup rotation degree
      matrix.postRotate(degree);

      return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
  }
}
