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

export function allAfterTaggedLine(text: string, tag: string, useRegex = false): string {
  return getAllBetweenLinesAsserting(text, makeLineAssertion(tag, useRegex), () => false)
}

export function allBetweenTaggedLines(
    text: string, startTag: string, endTag: string, useRegex = false): string {
  return getAllBetweenLinesAsserting(
    text, makeLineAssertion(startTag, useRegex), makeLineAssertion(endTag, useRegex))
}

type LineAssertion = (line: string) => boolean

function makeLineAssertion(tag: string, useRegex: boolean): LineAssertion {
  return (line: string) => useRegex ? new RegExp(tag).test(line) : line.includes(tag)
}

function getAllBetweenLinesAsserting(
    text: string, startLineAssertion: LineAssertion, endLineAssertion: LineAssertion): string {
  let result = ''

  let startLineFound = false
  let startAndEndLineFound = false
  text.split('\n').forEach(line => {
    if (startAndEndLineFound) {
      // Skipping all iterations after start and end line have been found
      return
    }

    if (startLineFound) {
      if (startAndEndLineFound = endLineAssertion(line)) {
        return
      }

      // Include the line if it is after the start-line and it isn't itself the end-line
      result += (line + '\n')
    } else {
      startLineFound = startLineAssertion(line)
    }
  })

  return result
}
