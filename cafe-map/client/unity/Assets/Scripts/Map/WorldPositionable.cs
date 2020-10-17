using System;
using CafeMap.Events;
using CafeMap.Player.Services;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class WorldPositionable : MonoBehaviour
    {
        public GameObject Prefab;

        public string Latitude;
        public string Longitude;

        private MapsService _mapsService;
        private ViewportService _viewportService;
        private SignalBus _signalBus;

        private Collider _collider;

        private void Awake()
        {
            _collider = gameObject.AddComponent<BoxCollider>();
        }

        [Inject]
        public void Init(MapsService mapsService, ViewportService viewportService, SignalBus signalBus)
        {
            _mapsService = mapsService;
            _viewportService = viewportService;
            _signalBus = signalBus;
        }

        private void Start()
        {
            var position = _mapsService.Coords.FromLatLngToVector3(new LatLng(double.Parse(Latitude), double.Parse(Longitude)));
            transform.position = position;

            if (Prefab != null)
            {
                Instantiate(Prefab, transform);
            }

            _mapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(args =>
            {
                if (_collider.bounds.Intersects(args.MapFeature.Shape.BoundingBox))
                {
                    args.Cancel = true;
                }
            });
        }

        private void Update()
        {
            if (transform.hasChanged)
            {
                var latlng = _mapsService.Coords.FromVector3ToLatLng(gameObject.transform.position);
                Latitude = latlng.Lat.ToString();
                Longitude = latlng.Lng.ToString();
            }
        }
    }
}
