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
        private DiContainer _container;

        [Inject]
        public void Init(SecretsService secretsService, MapsService mapsService, ViewportService viewportService, DiContainer container)
        {
            this.secretsService = secretsService;
            this.mapsService = mapsService;
            this.viewportService = viewportService;
            _container = container;
        }

        private void Start()
        {

            foreach (var place in secretsService.PlaceDb.Place)
            {
                var instantiated = Instantiate(model);
                var rendered = instantiated.GetComponentInChildren<Renderer>().gameObject.AddComponent<RenderedPlace>();
                _container.Inject(rendered);
                rendered.Place = place;
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
