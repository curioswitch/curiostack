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
        public string Latitude;
        public string Longitude;

        private MapsService _mapsService;
        private ViewportService _viewportService;
        private SignalBus _signalBus;

        private Renderer[] _renderers;
        private Bounds bounds;

        private void Awake()
        {
            _renderers = gameObject.GetComponentsInChildren<Renderer>();
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
            var gameObject = this.gameObject;
            gameObject.transform.position = position;

            recomputeBounds();

            _signalBus.Subscribe<MapOriginChanged>(recomputeBounds);

            _mapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(args =>
            {
                if (bounds.Intersects(args.MapFeature.Shape.BoundingBox))
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

                recomputeBounds();
            }
        }

        private void recomputeBounds()
        {
            Bounds bounds = new Bounds();
            foreach (var renderer in _renderers)
            {
                bounds.Encapsulate(renderer.bounds);
            }

            this.bounds = bounds;
        }
    }
}
