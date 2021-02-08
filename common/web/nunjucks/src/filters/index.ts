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

type LineAssertion = (line: string) => boolean;

function makeLineAssertion(
  lineMatcher: string,
  doInclusionCheck: boolean,
): LineAssertion {
  return (line: string) =>
    doInclusionCheck
      ? line.includes(lineMatcher)
      : new RegExp(lineMatcher).test(line);
}

function getAllBetweenLinesAsserting(
  text: string,
  startLineAssertion: LineAssertion,
  endLineAssertion: LineAssertion,
): string {
  let result = '';

  let startLineFound = false;
  let startAndEndLineFound = false;
  text.split('\n').forEach((line) => {
    if (startAndEndLineFound) {
      // Skipping all iterations after start and end line have been found
      return;
    }

    if (startLineFound) {
      if (endLineAssertion(line)) {
        startAndEndLineFound = true;
        return;
      }

      // Include the line if it is after the start-line and it isn't itself the end-line
      result += `${line}\n`;
    } else {
      startLineFound = startLineAssertion(line);
    }
  });

  return result;
}

export function allAfterLine(
  text: string,
  lineMatcher: string,
  doInclusionCheck = false,
): string {
  return getAllBetweenLinesAsserting(
    text,
    makeLineAssertion(lineMatcher, doInclusionCheck),
    () => false,
  );
}

export function allBetweenLines(
  text: string,
  lineMatcher: string,
  doInclusionCheck = false,
): string {
  return getAllBetweenLinesAsserting(
    text,
    makeLineAssertion(lineMatcher, doInclusionCheck),
    makeLineAssertion(lineMatcher, doInclusionCheck),
  );
}
