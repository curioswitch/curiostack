using System;
using System.Linq;
using CafeMap.Player.Services;
using Cysharp.Threading.Tasks;
using Google.Maps;
using Google.Maps.Coord;
using ModestTree;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class PlacesRenderer : MonoBehaviour
    {
        [SerializeField]
        private GameObject pinPrefab;

        private SecretsService secretsService;
        private MapsService mapsService;
        private ViewportService viewportService;
        private Canvas worldCanvas;
        private DiContainer _container;

        [Inject]
        public void Init(SecretsService secretsService, MapsService mapsService, ViewportService viewportService, Canvas worldCanvas, DiContainer container)
        {
            this.secretsService = secretsService;
            this.mapsService = mapsService;
            this.viewportService = viewportService;
            this.worldCanvas = worldCanvas;
            _container = container;
        }

        private void Start()
        {

            foreach (var place in secretsService.PlaceDb.Place.Where(place => !place.GooglePlaceId.IsEmpty()))
            {
                var instantiated = Instantiate(pinPrefab, worldCanvas.gameObject.transform);
                _container.InjectGameObject(instantiated);

                var rendered = instantiated.GetComponent<PlacePin>();
                rendered.Place = place;

                var latLng = new LatLng(place.Position.Latitude, place.Position.Longitude);
                var position = mapsService.Coords.FromLatLngToVector3(latLng);
                position.y = 50;
                instantiated.name = place.Name;
                instantiated.transform.position = position;
            }
        }
    }
}
