/* tslint:disable:max-classes-per-file */
/* tslint:disable:no-namespace */

interface AutoMessages {
  [key: string]: string;
}
namespace ReactIntl {
  function defineMessages<T extends AutoMessages>(messages: T): Messages;
}

declare module 'react-intl' {
  export = ReactIntl;
}

declare module '*.json' {
  const value: any;
  export default value;
}

declare module '*.png' {
  const value: string;
  export default value;
}

declare module '*.jpg' {
  const value: string;
  export default value;
}

declare module '*.svg' {
  const value: string;
  export default value;
}

declare module '*.eot' {
  const value: string;
  export default value;
}

declare module '*.ttf' {
  const value: string;
  export default value;
}

declare module '*.woff' {
  const value: string;
  export default value;
}

declare module '*.woff2' {
  const value: string;
  export default value;
}

declare module 'intl/locale-data/jsonp/*.js' {

}
