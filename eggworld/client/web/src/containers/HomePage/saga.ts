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

import { takeLatest } from '@curiostack/base-web';

import { grpc } from 'grpc-web-client';
import { all, AllEffect, call, put, select } from 'redux-saga/effects';

import {
  CheckIngredientsRequest,
  CheckIngredientsResponse,
  FindRecipeRequest,
  FindRecipeResponse,
} from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';
import { EggworldService } from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb_service';

import { Actions, ActionTypes } from './actions';

import selectHomePage from './selectors';

async function execute(method: any, request: any) {
  return new Promise((resolve: (param: any) => void, reject) =>
    grpc.unary(method, {
      request,
      host: '/api',
      onEnd: (response) => {
        if (response.status !== grpc.Code.OK) {
          reject(
            new Error(
              `Error communicating with API. grpc-status: ${
                response.status
              } grpc-message: ${response.statusMessage}`,
            ),
          );
        } else {
          resolve(response.message);
        }
      },
    }),
  );
}

function* doCheckIngredients({
  payload: ingredients,
}: ReturnType<typeof Actions.checkIngredients>) {
  const request = new CheckIngredientsRequest();
  request.setSelectedIngredientList(ingredients);
  const response: CheckIngredientsResponse = yield call(() =>
    execute(EggworldService.CheckIngredients, request),
  );
  yield put(Actions.checkIngredientsResponse(response));
}

function* doCook() {
  const state = yield select(selectHomePage);
  const request = new FindRecipeRequest();
  request.setIngredientList(state.eatenFood);
  const response: FindRecipeResponse = yield call(() =>
    execute(EggworldService.FindRecipe, request),
  );
  yield put(Actions.cookResponse(response.getRecipeUrl()));
}

// Individual exports for testing
export default function* rootSaga(): IterableIterator<AllEffect> {
  yield all([
    takeLatest(ActionTypes.CHECK_INGREDIENTS, doCheckIngredients),
    takeLatest(ActionTypes.COOK, doCook),
  ]);
}
