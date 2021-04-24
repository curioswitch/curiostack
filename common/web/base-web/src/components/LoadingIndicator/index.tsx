/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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

import React from 'react';
import styled from 'styled-components';

import Circle from './Circle';

const Wrapper = styled.div`
  margin: 2em auto;
  width: 40px;
  height: 40px;
  position: relative;
`;

const LoadingIndicator = () => (
  <Wrapper>
    <Circle />
    <Circle rotate={30} delay={-1.1} />
    <Circle rotate={60} delay={-1} />
    <Circle rotate={90} delay={-0.9} />
    <Circle rotate={120} delay={-0.8} />
    <Circle rotate={150} delay={-0.7} />
    <Circle rotate={180} delay={-0.6} />
    <Circle rotate={210} delay={-0.5} />
    <Circle rotate={240} delay={-0.4} />
    <Circle rotate={270} delay={-0.3} />
    <Circle rotate={300} delay={-0.2} />
    <Circle rotate={330} delay={-0.1} />
  </Wrapper>
);

export default LoadingIndicator;
