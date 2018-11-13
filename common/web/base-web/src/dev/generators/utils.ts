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

import fs from 'fs';
import os from 'os';
import path from 'path';
import { promisify } from 'util';

import handlebars from 'handlebars';

const writeFile = promisify(fs.writeFile);

const componentsPath = path.resolve(process.cwd(), 'src/components');
const pageComponents = fs.existsSync(componentsPath)
  ? fs.readdirSync(componentsPath)
  : [];

const containersPath = path.resolve(process.cwd(), 'src/containers');
const pageContainers = fs.existsSync(containersPath)
  ? fs.readdirSync(containersPath)
  : [];
const components = [...pageComponents, ...pageContainers];

export function componentExists(comp: string) {
  return components.indexOf(comp) >= 0;
}

handlebars.registerHelper('properCase', (val: string) =>
  val.length > 0 ? val[0].toUpperCase() + val.substring(1) : '',
);

handlebars.registerHelper('camelCase', (val: string) =>
  val.length > 0 ? val[0].toLowerCase() + val.substring(1) : '',
);

handlebars.registerHelper('curly', (_: any, open: boolean) =>
  open ? '{' : '}',
);

function findLicenseHeader() {
  let dir = process.cwd();
  while (dir !== '/' && dir !== os.homedir()) {
    const licensePath = path.resolve(dir, 'LICENSE');
    if (fs.existsSync(licensePath)) {
      const licenseFile = fs.readFileSync(licensePath, { encoding: 'utf8' });
      const lineEnd = licenseFile.indexOf('\r\n') < 0 ? '\n' : '\r\n';
      return `/*
${licenseFile
        .split(lineEnd)
        .map((line) => ` ${line ? `* ${line}` : '*'}`)
        // We force all files in this repository to LF, so the published templates should be LF on
        // either Unix or Windows. If this causes problems on Windows, we'll need to post-filter
        // all generated files with line-ending fix.
        .join('\n')}
 */

`;
    }
    dir = path.resolve(dir, '..');
  }
  return '';
}

export const licenseHeader = findLicenseHeader();

export async function renderTemplate(
  template: Buffer,
  outPath: string,
  context: object,
) {
  const rendered = handlebars.compile(template.toString())(context);
  return writeFile(
    outPath,
    outPath.endsWith('.json') ? rendered : licenseHeader + rendered,
  );
}

export type TypeArg = 'normal' | 'stateless' | 'pure';
export type Type =
  | 'React.Component'
  | 'Stateless Function'
  | 'React.PureComponent';

export function convertTypeArg(type: TypeArg): Type {
  switch (type) {
    case 'normal':
      return 'React.Component';
    case 'stateless':
      return 'Stateless Function';
    case 'pure':
      return 'React.PureComponent';
    default:
      throw new Error(`Unknown type: ${type}`);
  }
}
