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

import path from 'path';

import { Configuration, Linter } from 'tslint';
import typescript, { FormatDiagnosticsHost } from 'typescript';

import { test } from './test';

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
  const program = Linter.createProgram(
    path.resolve(process.cwd(), 'tsconfig.json'),
    process.cwd(),
  );

  const linter = new Linter(
    {
      fix: !!fix,
    },
    program,
  );

  const diagnostics = [
    ...program.getSemanticDiagnostics(),
    ...program.getSyntacticDiagnostics(),
  ];
  if (diagnostics.length > 0) {
    console.log(
      typescript.formatDiagnosticsWithColorAndContext(
        diagnostics,
        new FormatTypescriptHost(),
      ),
    );
    process.exit(1);
  }

  for (const file of Linter.getFileNames(program)) {
    const sourceFile = program.getSourceFile(file);
    const content = sourceFile!.text;
    const configuration = Configuration.findConfiguration(null, file).results;
    linter.lint(file, content, configuration);
  }

  const results = linter.getResult();
  console.log(results.output);
  return results.errorCount === 0;
}

export async function check() {
  lint();
  return test();
}

if (require.main === module) {
  check().catch(() => process.exit(1));
}
