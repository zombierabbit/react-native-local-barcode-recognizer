
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "RNReactNativeLocalBarcodeRecognizer.h"
#import <ZXingObjC/ZXingObjC.h>

#include <stdlib.h>

@implementation RNReactNativeLocalBarcodeRecognizer

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (NSDictionary *)validCodeTypes
{
    return @{
             @"upc_e" : AVMetadataObjectTypeUPCECode,
             @"code39" : AVMetadataObjectTypeCode39Code,
             @"code39mod43" : AVMetadataObjectTypeCode39Mod43Code,
             @"ean13" : AVMetadataObjectTypeEAN13Code,
             @"ean8" : AVMetadataObjectTypeEAN8Code,
             @"code93" : AVMetadataObjectTypeCode93Code,
             @"code128" : AVMetadataObjectTypeCode128Code,
             @"pdf417" : AVMetadataObjectTypePDF417Code,
             @"qr" : AVMetadataObjectTypeQRCode,
             @"aztec" : AVMetadataObjectTypeAztecCode,
             @"interleaved2of5" : AVMetadataObjectTypeInterleaved2of5Code,
             @"itf14" : AVMetadataObjectTypeITF14Code,
             @"datamatrix" : AVMetadataObjectTypeDataMatrixCode
             };
}

RCT_EXPORT_MODULE(LocalBarcodeRecognizer);

RCT_EXPORT_METHOD(decode:(NSString *)base64EncodedImage
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    int queueIndex = arc4random_uniform(74);
    const char *queueName = [[NSString stringWithFormat:@"%d",queueIndex] UTF8String];
    
    dispatch_queue_t queue = dispatch_queue_create(queueName, NULL);
    dispatch_async(queue, ^{
        UIImage* image =[self decodeBase64ToImage:base64EncodedImage];
        
        for (int i = 0; i < 4; i++)
        {
            ZXResult* result = [self decodeBarCode:image];
            
            if(result){
                resolve(result.text);
                return;
            }
            image = [self rotateImage:image clockwise:YES];
        }
        
        resolve(@"");
    });
}

-(ZXResult *)decodeBarCode:(UIImage*)image
{
    ZXLuminanceSource *source = [[ZXCGImageLuminanceSource alloc] initWithCGImage:image.CGImage];
    ZXBinaryBitmap *bitmap = [ZXBinaryBitmap binaryBitmapWithBinarizer:[ZXHybridBinarizer binarizerWithSource:source]];
    
    // There are a number of hints we can give to the reader, including
    // possible formats, allowed lengths, and the string encoding.
    ZXDecodeHints *hints = [ZXDecodeHints hints];
    NSError *error = nil;

    ZXMultiFormatReader *reader = [ZXMultiFormatReader reader];
    return [reader decode:bitmap hints:hints error:&error];
}

- (UIImage *)decodeBase64ToImage:(NSString *)strEncodeData {
    NSData *data = [[NSData alloc]initWithBase64EncodedString:strEncodeData options:NSDataBase64DecodingIgnoreUnknownCharacters];
    return [UIImage imageWithData:data];
}

- (UIImage*)rotateImage:(UIImage*)sourceImage clockwise:(BOOL)clockwise
{
    CGSize size = sourceImage.size;
    UIGraphicsBeginImageContext(CGSizeMake(size.height, size.width));
    [[UIImage imageWithCGImage:[sourceImage CGImage]
                         scale:1.0
                   orientation:clockwise ? UIImageOrientationRight : UIImageOrientationLeft]
     drawInRect:CGRectMake(0,0,size.height ,size.width)];
    
    UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return newImage;
}

@end
  
