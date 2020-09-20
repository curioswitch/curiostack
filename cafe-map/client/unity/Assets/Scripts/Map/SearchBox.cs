using System;
using Google.Maps;
using Google.Maps.Coord;
using GoogleApi;
using GoogleApi.Entities.Places.Search.Common.Enums;
using GoogleApi.Entities.Places.Search.Text.Request;
using UnityEngine;
using UnityEngine.UI;

namespace CafeMap.Map
{
    public class SearchBox : MonoBehaviour
    {
        private InputField input;
        private MapsService mapsService;
        private PanAndZoom panAndZoom;

        private void Start()
        {
            input = GetComponent<InputField>();
            mapsService = GameObject.Find("Map Base").GetComponent<MapsService>();
            panAndZoom = Camera.main.GetComponent<PanAndZoom>();

            input.onEndEdit.AddListener((text) =>
            {
                search(text);
            });
        }

        private async void search(string query)
        {
            var request = new PlacesTextSearchRequest
            {
                Key = KeysInitializer.GoogleApiKey,
                Query = query,
            };

            var response = await GooglePlaces.TextSearch.QueryAsync(request);
            foreach (var result in response.Results)
            {
                var location = result.Geometry.Location;
                var latlng = new LatLng(location.Latitude, location.Longitude);
                mapsService.MoveFloatingOrigin(latlng);
                var coords = mapsService.Coords.FromLatLngToVector3(latlng);
                panAndZoom.SetPosition(coords);
                break;
            }
            Debug.Log(response.Results);
        }
    }
}