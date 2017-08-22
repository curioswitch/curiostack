import type {
  CheckIngredientsResponse,
  Ingredient,
} from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { grpc, Code } from 'grpc-web-client';
import { takeLatest, call, put } from 'redux-saga/effects';

import { CheckIngredientsRequest } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';
import { EggworldService } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb_service';

import {
  checkIngredients,
  checkIngredientsResponse,
} from './actions';

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

// Individual exports for testing
export default function* defaultSaga() {
  yield [
    takeLatest(checkIngredients.toString(), doCheckIngredients),
  ];
}
