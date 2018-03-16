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

import fs from 'fs';
import { copy, mkdirs } from 'fs-extra';
import path from 'path';
import { promisify } from 'util';

import program from 'commander';
import inquirer, { Question } from 'inquirer';

import { renderTemplate } from '../utils';

import baseWebDevPackageJson from '../../../../base-web-dev/package.json';
import baseWebPackageJson from '../../../../base-web/package.json';

import packageJson from '../../../package.json';

const readFile = promisify(fs.readFile);

let argDir: string | undefined;

program
  .version(packageJson.version)
  .arguments('[dir]')
  .action((dir: string) => (argDir = dir))
  .option('-n, --name <name>', 'Name of the package')
  .parse(process.argv);

async function run() {
  const packageJsonTemplate = await readFile(
    path.join(__dirname, 'package.json.hbs'),
  );
  const tsconfigTemplate = await readFile(
    path.join(__dirname, 'tsconfig.json.hbs'),
  );
  const baseTsConfigTemplate = await readFile(
    path.join(__dirname, 'tsconfig.json'),
  );
  const tslintConfigTemplate = await readFile(
    path.join(__dirname, 'tslint.json.hbs'),
  );

  const questions: Question[] = [];
  let dir: string | undefined = argDir;
  if (!dir) {
    questions.push({
      name: 'dir',
      message: 'Enter the directory to create the package in',
    });
  }
  let name = program.naame;
  if (!name) {
    questions.push({
      name: 'name',
      message: 'What is the name of the package?',
    });
  }

  if (questions.length > 0) {
    const answers = await inquirer.prompt(questions);
    if (answers.dir) {
      dir = answers.dir;
    }
    if (answers.name) {
      name = answers.name;
    }
  }

  const packageDir = path.resolve(process.cwd(), dir);
  const context = {
    name,
    baseWebVersion: baseWebPackageJson.version,
    baseWebDevVersion: baseWebDevPackageJson.version,
    baseTsConfigLocation: path.join(
      path.relative(packageDir, process.cwd()),
      'tsconfig.json',
    ),
  };
  await mkdirs(path.resolve(packageDir));
  await renderTemplate(
    packageJsonTemplate,
    path.resolve(packageDir, 'package.json'),
    context,
  );
  await renderTemplate(
    tslintConfigTemplate,
    path.resolve(packageDir, 'tslint.json'),
    context,
  );
  if (packageDir === path.resolve(process.cwd())) {
    await renderTemplate(
      baseTsConfigTemplate,
      path.resolve(packageDir, 'tsconfig.json'),
      context,
    );
  } else {
    await renderTemplate(
      tsconfigTemplate,
      path.resolve(packageDir, 'tsconfig.json'),
      context,
    );
  }
  await copy(
    path.join(__dirname, 'src-template'),
    path.resolve(packageDir, 'src'),
  );
}

run()
  .then(() => {
    process.exit(0);
  })
  .catch((err) => {
    console.log('Error running script.', err);
    process.exit(1);
  });
