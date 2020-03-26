#!/usr/bin/env node
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
/**
 * @license
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import './common';

import fs from 'fs';
import path from 'path';

import { CLIEngine } from 'eslint';
import typescript, { FormatDiagnosticsHost } from 'typescript';

import test from './test';

class FormatTypescriptHost implements FormatDiagnosticsHost {
  public getCurrentDirectory(): string {
    return process.cwd();
  }

  public getCanonicalFileName(fileName: string): string {
    return fileName;
  }

  public getNewLine(): string {
    return '\n';
  }
}

export function lint(fix?: boolean) {
  const config = typescript.readConfigFile(
    path.resolve(process.cwd(), 'tsconfig.json'),
    typescript.sys.readFile,
  );

  const formatter = new FormatTypescriptHost();

  if (config.error) {
    // eslint-disable-next-line no-console
    console.error(
      typescript.formatDiagnosticsWithColorAndContext(
        [config.error],
        formatter,
      ),
    );
    process.exit(1);
  }

  const parsed = typescript.parseJsonConfigFileContent(
    config.config,
    {
      fileExists: fs.existsSync,
      readDirectory: typescript.sys.readDirectory,
      readFile: (file) => fs.readFileSync(file, 'utf8'),
      useCaseSensitiveFileNames: true,
    },
    process.cwd(),
    { noEmit: true },
  );
  if (parsed.errors) {
    // ignore warnings and 'TS18003: No inputs were found in config file ...'
    const errors = parsed.errors.filter(
      (d) =>
        d.category === typescript.DiagnosticCategory.Error && d.code !== 18003,
    );
    if (errors.length !== 0) {
      // eslint-disable-next-line no-console
      console.error(
        typescript.formatDiagnosticsWithColorAndContext(errors, formatter),
      );
      process.exit(1);
    }
  }

  const host = typescript.createCompilerHost(parsed.options, true);
  const program = typescript.createProgram(
    parsed.fileNames,
    parsed.options,
    host,
  );

  const diagnostics = [
    ...program.getSemanticDiagnostics(),
    ...program.getSyntacticDiagnostics(),
  ];
  if (diagnostics.length > 0) {
    // eslint-disable-next-line no-console
    console.log(
      typescript.formatDiagnosticsWithColorAndContext(
        diagnostics,
        new FormatTypescriptHost(),
      ),
    );
    process.exit(1);
  }

  const lintCli = new CLIEngine({
    fix: !!fix,
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
    ignorePattern: ['*.d.ts'],
  });

  const report = lintCli.executeOnFiles(['src/']);
  if (fix) {
    CLIEngine.outputFixes(report);
  }
  // eslint-disable-next-line no-console
  console.log(lintCli.getFormatter()(report.results));
  return report.errorCount === 0;
}

export async function check() {
  const result = await lint();
  if (!result) {
    throw new Error('Lint failed.');
  }
  return test();
}

if (require.main === module) {
  check().catch(() => process.exit(1));
}
