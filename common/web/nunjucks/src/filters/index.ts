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
  exactMatch: boolean,
): LineAssertion {
  const lineRegex = new RegExp(lineMatcher);

  const matchExactly = (lineToMatch: string): boolean => {
    const match = lineToMatch.match(lineRegex);
    return match != null && match[0] === lineToMatch;
  };

  return (line: string) =>
    exactMatch ? matchExactly(line) : lineRegex.test(line);
}

function getTextBlocksBetweenLines(
  text: string,
  startLineAssertion: LineAssertion,
  endLineAssertion: LineAssertion,
): string[] {
  const textBlocks = [] as string[];

  let insideTextBlock = false;
  let textBlockLines = [] as string[];

  const addTextBlock = (lines: string[]) => {
    textBlocks.push(lines.join('\n'));
  };

  text.split('\n').forEach((line) => {
    if (insideTextBlock) {
      if (endLineAssertion(line)) {
        // finish a text block and add it to textBlocks array, if an end-line has been found
        insideTextBlock = false;
        addTextBlock(textBlockLines);
        textBlockLines = [];
        return;
      }

      // include the line in the textBlockLines array if currently insideTextBlock
      // after the start-line, and this isn't the end-line
      textBlockLines.push(line);
    } else if (startLineAssertion(line)) {
      insideTextBlock = true;
    }
  });

  if (textBlockLines.length > 0) {
    // add last text block if it wasn't closed, and the end was reached
    addTextBlock(textBlockLines);
  }

  return textBlocks;
}

export function allAfterLine(
  text: string,
  lineMatcher: string,
  exactMatch = false,
): string {
  const textBlocks = getTextBlocksBetweenLines(
    text,
    makeLineAssertion(lineMatcher, exactMatch),
    () => false,
  );
  return textBlocks[0] || '';
}

export function allBetweenLines(
  text: string,
  lineMatcher: string,
  exactMatch = false,
): string {
  const textBlocks = getTextBlocksBetweenLines(
    text,
    makeLineAssertion(lineMatcher, exactMatch),
    makeLineAssertion(lineMatcher, exactMatch),
  );
  return textBlocks.join('\n');
}
