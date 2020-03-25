/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import { getOptions, interpolateName, parseQuery } from 'loader-utils';
import sharp, { JpegOptions, PngOptions, WebpOptions } from 'sharp';
import { loader } from 'webpack';

import LoaderContext = loader.LoaderContext;

interface ResourceOptions {
  lossy: boolean;
  [breakpoint: string]: any; // Actually number, but can't be used with boolean in same object.
}

interface LoaderOptions {
  deviceWidths: number[];
  breakpoints: {
    [breakpoint: string]: string;
  };
  jpeg: JpegOptions;
  png: PngOptions;
  webp: WebpOptions;
}

const DEFAULT_OPTIONS: LoaderOptions = {
  deviceWidths: [768, 1080, 1440, 1920, 4096],
  breakpoints: {
    xs: '0px',
    sm: '600px',
    md: '960px',
    lg: '1280px',
    xl: '1920px',
  },
  jpeg: {
    progressive: true,
    trellisQuantisation: true,
    overshootDeringing: true,
    optimizeScans: true,
  },
  png: {
    adaptiveFiltering: true,
  },
  webp: {
    quality: 80,
    alphaQuality: 100,
    lossless: true,
  },
};

async function createImage(
  ctx: LoaderContext,
  options: LoaderOptions,
  content: Buffer,
  format: string,
  width: number,
) {
  let s = sharp(content).resize(width).toFormat(format);
  if (format === 'jpg') {
    s = s.jpeg(options.jpeg);
  } else if (format === 'png') {
    s = s.png(options.png);
  } else if (format === 'webp') {
    s = s.webp(options.webp);
  }
  const converted = await s.toBuffer();
  const outputPath = interpolateName(ctx, `[name].${width}.[hash].${format}`, {
    context: ctx.context,
    content: converted,
  });
  ctx.emitFile(outputPath, converted, undefined);
  // eslint-disable-next-line no-underscore-dangle
  const publicPathDir = ctx._compiler.options.output!.publicPath || '';
  const publicPath = `${publicPathDir}${outputPath}`;
  return { format, width, path: publicPath };
}

async function run(ctx: LoaderContext, content: Buffer) {
  ctx.async();
  ctx.cacheable(true);

  if (!ctx.resourceQuery) {
    // Passthrough when no options specified.
    const outputPath = interpolateName(ctx, '[name].[hash].[ext]', {
      content,
      context: ctx.context,
    });
    ctx.emitFile(outputPath, content, undefined);
    const publicPath = `__webpack_public_path__ + ${JSON.stringify(
      outputPath,
    )}`;
    ctx.callback(undefined, `export default ${publicPath}`);
    return;
  }

  const resourceOptions = parseQuery(ctx.resourceQuery) as ResourceOptions;

  const loaderOptions: LoaderOptions = {
    ...DEFAULT_OPTIONS,
    ...getOptions(ctx),
  };

  const { lossy, ...breakpoints } = resourceOptions;
  if (lossy) {
    loaderOptions.webp.lossless = false;
  }
  const formats = ['webp', lossy ? 'jpg' : 'png'];
  const deviceWidths = loaderOptions.deviceWidths;
  const widths = Object.keys(breakpoints)
    .map((key) => breakpoints[key])
    .map((viewportWidth: number) => viewportWidth / 100)
    .reduce(
      (values: number[], value: number) => [
        ...values,
        ...deviceWidths.map((deviceWidth: number) =>
          Math.ceil(deviceWidth * value),
        ),
      ],
      [],
    );
  const createImagePromises = [];
  for (const format of formats) {
    for (const width of widths) {
      createImagePromises.push(
        createImage(ctx, loaderOptions, content, format, width),
      );
    }
  }
  const results = await Promise.all(createImagePromises);

  const reversedBreakpoints = Object.keys(breakpoints).reverse();
  const sizes = reversedBreakpoints
    .map((breakpoint, i) =>
      i !== reversedBreakpoints.length - 1
        ? `(min-width: ${loaderOptions.breakpoints[breakpoint]}) ${breakpoints[breakpoint]}vw`
        : `${breakpoints[breakpoint]}vw`,
    )
    .join(', ');
  const sources = formats.map((format) => ({
    sizes,
    type: format === 'jpg' ? 'image/jpeg' : `image/${format}`,
    srcSet: results
      .filter((image) => image.format === format)
      .sort((a, b) => b.width - a.width)
      .map((image) => `${image.path} ${image.width}w`)
      .join(', '),
  }));

  const fallback = results
    .filter((image) => image.format !== 'webp')
    .sort((a, b) => a.width - b.width)[0].path;
  const output = {
    sources,
    fallback,
  };

  ctx.callback(undefined, `module.exports = ${JSON.stringify(output)}`);
}

export default function curioImageLoader(this: LoaderContext, content: Buffer) {
  run(this, content).catch((e) => this.callback(e));
}

export const raw = true;
