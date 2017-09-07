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

import type {
  CheckIngredientsResponse,
  FindRecipeResponse,
  Ingredient,
} from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { grpc, Code } from 'grpc-web-client';
import { takeLatest, call, put, select } from 'redux-saga/effects';

import { CheckIngredientsRequest, FindRecipeRequest } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';
import { EggworldService } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb_service';

import {
  checkIngredients,
  checkIngredientsResponse,
  cook,
  cookResponse,
} from './actions';

import makeSelectHomePage from './selectors';

const selectHomePage = makeSelectHomePage();

async function execute(method: any, request: any) {
  return new Promise((resolve, reject) =>
    grpc.unary(method, {
      host: '/api',
      request,
      onEnd: (response) => {
        if (response.status !== Code.OK) {
          reject(new Error(`Error communicating with API. grpc-status: ${response.status} grpc-message: ${response.statusMessage}`));
        } else {
          resolve(response.message);
        }
      },
    })
  );
}

function* doCheckIngredients({ payload }: { payload: Ingredient[] }) {
  const request = new CheckIngredientsRequest();
  request.setSelectedIngredientList(payload);
  const response: CheckIngredientsResponse = yield call(() => execute(EggworldService.CheckIngredients, request));
  yield put(checkIngredientsResponse(response));
}

function* doCook() {
  const state = yield select(selectHomePage);
  const request = new FindRecipeRequest();
  request.setIngredientList(state.eatenFood);
  const response: FindRecipeResponse = yield call(() => execute(EggworldService.FindRecipe, request));
  yield put(cookResponse(response.getRecipeUrl()));
}

// Individual exports for testing
export default function* defaultSaga() {
  yield [
    takeLatest(checkIngredients.toString(), doCheckIngredients),
    takeLatest(cook.toString(), doCook),
  ];
}
