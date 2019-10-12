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

import Konva from 'konva';
import React, { useEffect, useState } from 'react';
import { Image as ReactKonvaImage, KonvaNodeEvents } from 'react-konva';

type ImageConfig = Pick<
  Konva.ImageConfig,
  Exclude<keyof Konva.ImageConfig, { image: any }>
>;

interface Props extends KonvaNodeEvents {
  src: string;
}

const KonvaImage: React.FunctionComponent<Props | ImageConfig> = (props) => {
  const { src } = props;

  const [image, setImage] = useState<HTMLImageElement>();

  useEffect(() => {
    const domImage = new Image();
    domImage.onload = () => {
      if (!image) {
        setImage(domImage);
      }
    };
    domImage.src = src;
  }, []);

  useEffect(() => {
    if (!image) {
      return;
    }

    image.src = src;
  }, [src, image]);

  if (!image) {
    return null;
  }

  // eslint-disable-next-line react/jsx-props-no-spreading
  return <ReactKonvaImage image={image} {...props} />;
};

export default React.memo(KonvaImage);
