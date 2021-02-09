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
  const matchExactly = (lineToCheck: string): boolean => {
    const match = lineToCheck.match(new RegExp(lineMatcher));
    return match != null && match[0] === lineToCheck;
  };

  return (line: string) =>
    doInclusionCheck ? line.includes(lineMatcher) : matchExactly(line);
}

function getTextBlocksBetweenLines(
  text: string,
  startLineAssertion: LineAssertion,
  endLineAssertion: LineAssertion,
): string[] {
  const textBlocks = [] as string[];

  let includeMode = false;
  let blockOfLines = [] as string[];

  const addTextBlock = (lines: string[]) => {
    textBlocks.push(lines.join('\n'));
  };

  text.split('\n').forEach((line) => {
    if (includeMode) {
      if (endLineAssertion(line)) {
        // stop includeMode and finish a text block, if an end-line has been found
        includeMode = false;
        addTextBlock(blockOfLines);
        blockOfLines = [];
        return;
      }

      // include the line in the blockOfLines if in includeMode after the start-line,
      // and this isn't the end-line
      blockOfLines.push(line);
    } else {
      includeMode = startLineAssertion(line);
    }
  });

  if (blockOfLines.length > 0) {
    addTextBlock(blockOfLines);
  }

  return textBlocks;
}

export function allAfterLine(
  text: string,
  lineMatcher: string,
  doInclusionCheck = false,
): string {
  const textBlocks = getTextBlocksBetweenLines(
    text,
    makeLineAssertion(lineMatcher, doInclusionCheck),
    () => false,
  );
  return textBlocks[0] || '';
}

export function allBetweenLines(
  text: string,
  lineMatcher: string,
  doInclusionCheck = false,
): string {
  const textBlocks = getTextBlocksBetweenLines(
    text,
    makeLineAssertion(lineMatcher, doInclusionCheck),
    makeLineAssertion(lineMatcher, doInclusionCheck),
  );
  return textBlocks.join('\n');
}
