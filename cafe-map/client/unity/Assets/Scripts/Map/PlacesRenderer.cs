using System;
using CafeMap.Player.Services;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class PlacesRenderer : MonoBehaviour
    {
        public GameObject model;
        
        private SecretsService secretsService;
        private MapsService mapsService;
        private ViewportService viewportService;

        [Inject]
        public void Init(SecretsService secretsService, MapsService mapsService, ViewportService viewportService)
        {
            this.secretsService = secretsService;
            this.mapsService = mapsService;
            this.viewportService = viewportService;
        }

        private void Start()
        {
            
            foreach (var place in secretsService.PlaceDb.Place)
            {
                var instantiated = Instantiate(model);
                var latLng = new LatLng(place.Position.Latitude, place.Position.Longitude);
                var position = mapsService.Coords.FromLatLngToVector3(latLng);
                instantiated.name = place.Name;
                instantiated.transform.position = position;
                instantiated.transform.localScale = new Vector3(10, 10, 10);
                viewportService.RegisterMovedObject(instantiated);
            }
        }
    }
}