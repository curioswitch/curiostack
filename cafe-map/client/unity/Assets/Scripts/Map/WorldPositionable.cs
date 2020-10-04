using System;
using CafeMap.Player.Services;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class WorldPositionable : MonoBehaviour
    {

        public double Latitude;
        public double Longitude;
        public float Rotation;
        public float Scale;

        private MapsService _mapsService;
        private ViewportService _viewportService;

        [Inject]
        public void Init(MapsService mapsService, ViewportService viewportService)
        {
            _mapsService = mapsService;
            _viewportService = viewportService;
        }

        private void Start()
        {
            var position = _mapsService.Coords.FromLatLngToVector3(new LatLng(Latitude, Longitude));
            var gameObject = this.gameObject;
            gameObject.transform.position = position;
            gameObject.transform.Rotate(Vector3.up, Rotation);
            gameObject.transform.localScale = new Vector3(Scale, Scale, Scale);
            _viewportService.RegisterMovedObject(gameObject);
        }

        private void Update()
        {
            if (transform.hasChanged)
            {
                var latlng = _mapsService.Coords.FromVector3ToLatLng(gameObject.transform.position);
                Latitude = latlng.Lat;
                Longitude = latlng.Lng;
                Rotation = gameObject.transform.rotation.eulerAngles.y;
                Scale = Math.Min(gameObject.transform.localScale.x,
                    Math.Min(gameObject.transform.localScale.y, gameObject.transform.localScale.z));
            }
        }
    }
}
