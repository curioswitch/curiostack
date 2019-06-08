/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

import React, { ReactNode, useCallback } from 'react';
import SwipeableViews from 'react-swipeable-views';
import styled from 'styled-components';

export enum SheetVisibility {
  HIDDEN,
  CLOSED,
  OPEN,
}

export interface Props {
  children?: ReactNode;
  visibility: SheetVisibility;
  setOpen: (open: boolean) => void;
}

const Container = styled.div`
  height: 100px;
  position: fixed;
  bottom: 0;
  right: 0;
  left: 0;
  z-index: 200;
`;

const SheetContent = styled.div<{ open: boolean }>`
  overflow: ${(props) => (props.open ? 'auto' : 'hidden')};
  background-color: white;
  height: ${window.innerHeight}px;
`;

const BottomSlide = styled.div`
  margin-bottom: 100px;
`;

const swiperStyle = {
  overflowY: 'initial' as const,
};

const slideStyle = {
  overflow: 'visibility',
  marginBottom: '-100px',
};

const BottomSheet: React.FunctionComponent<Props> = ({
  children,
  setOpen,
  visibility,
}) => {
  if (visibility === SheetVisibility.HIDDEN) {
    return null;
  }

  const onChangeIndex = useCallback(
    (index: number) => {
      const open = index === 1;
      setOpen(open);
    },
    [setOpen],
  );

  const index = visibility === SheetVisibility.OPEN ? 1 : 0;

  return (
    <Container>
      <SwipeableViews
        enableMouseEvents
        axis="y"
        hysteresis={0.3}
        index={index}
        onChangeIndex={onChangeIndex}
        style={swiperStyle}
        slideStyle={slideStyle}
      >
        <SheetContent open={visibility === SheetVisibility.OPEN}>
          {children}
        </SheetContent>
        <BottomSlide />
      </SwipeableViews>
    </Container>
  );
};

export default React.memo(BottomSheet);
