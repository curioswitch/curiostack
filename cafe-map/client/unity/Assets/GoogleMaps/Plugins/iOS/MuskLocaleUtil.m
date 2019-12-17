#include "MuskLocaleUtil.h"

#import <Foundation/Foundation.h>

char* MuskGetLocaleRegion() {
  NSLocale *locale = [NSLocale currentLocale];
  if (locale == NULL) {
    return NULL;
  }
  NSString *countryCode = [locale objectForKey: NSLocaleCountryCode];
  if (countryCode == NULL) {
    return NULL;
  }

  /* .NET string marshalling takes ownership of the string, so copy it. */
  unsigned long length =
      [countryCode lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
  if (length <= 0) {
    return NULL;
  }
  char* countryCodeCopy = (char*) malloc(length + 1);
  strncpy(countryCodeCopy, [countryCode UTF8String], length);
  countryCodeCopy[length] = '\0';
  return countryCodeCopy;
}
