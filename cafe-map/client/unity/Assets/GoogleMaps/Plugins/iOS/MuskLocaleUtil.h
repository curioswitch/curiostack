/* Locale utility functions for Maps Unity SDK on iOS. */
#ifndef MUSK_LOCALE_UTIL_H_ /* NOLINT(build/header_guard) */
#define MUSK_LOCALE_UTIL_H_

#ifdef __cplusplus
extern "C" {
#endif  /* __cplusplus */

  // Gets the ISO 3166-1 alpha-2 country code for this device's locale.
  char* MuskGetLocaleRegion();

#ifdef __cplusplus
}
#endif  /* __cplusplus */

#endif  /* MUSK_LOCALE_UTIL_H_ */ /* NOLINT(build/header_guard) */
