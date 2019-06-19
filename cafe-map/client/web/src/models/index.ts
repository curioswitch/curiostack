/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

import { Place as CafeMapPlace } from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

export class Place {
  private readonly cafeMapPlace: CafeMapPlace;

  private readonly googleMapsPlace: google.maps.places.PlaceResult;

  public constructor(
    cafeMapPlace: CafeMapPlace,
    googleMapsPlace: google.maps.places.PlaceResult,
  ) {
    this.cafeMapPlace = cafeMapPlace;
    this.googleMapsPlace = googleMapsPlace;
  }

  public getId(): string {
    return this.cafeMapPlace.getId();
  }

  public getName(): string {
    return this.googleMapsPlace.name;
  }

  public getPrimaryPhotoUrl(): string {
    return this.googleMapsPlace.photos![0]!.getUrl({});
  }

  public getPosition(): google.maps.LatLng {
    const location = this.googleMapsPlace.geometry!.location!;
    return new google.maps.LatLng(location.lat(), location.lng());
  }
}
