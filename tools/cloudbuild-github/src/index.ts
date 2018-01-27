/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

import * as sourceMapSupport from 'source-map-support';
sourceMapSupport.install();

import { Request, Response } from 'express-serve-static-core';
import * as HttpStatus from 'http-status-codes';

import { handleBuildEvent } from './notifier';
import { handleWebhook } from './webhook';

export interface ICloudFunctionsRequest extends Request {
  rawBody: Buffer;
}

export function cloudbuildGithubWebhook(
  req: ICloudFunctionsRequest,
  res: Response,
) {
  handleWebhook(req, res).catch((err) => {
    console.error('Error handling webhook: ', err);
    res.status(HttpStatus.INTERNAL_SERVER_ERROR).end();
  });
}

export function cloudbuildGithubNotifier(event: any) {
  return handleBuildEvent(event);
}
