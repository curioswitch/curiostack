using System.Collections.Generic;
using CafeMap.Events;
using CafeMap.Map;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using Zenject;

namespace CafeMap.Player.Services
{
    public class ViewportService
    {
        private readonly MapsService mapsService;
        private readonly PanAndZoom cameraControl;
        private readonly DynamicMapsUpdater mapsUpdater;
        private readonly SignalBus _signalBus;

        private readonly List<GameObject> movedObjects;

        [Inject]
        public ViewportService(MapsService mapsService, PanAndZoom cameraControl, DynamicMapsUpdater mapsUpdater, SignalBus signalBus)
        {
            this.mapsService = mapsService;
            this.cameraControl = cameraControl;
            this.mapsUpdater = mapsUpdater;
            _signalBus = signalBus;
            movedObjects = new List<GameObject>();
        }

        public void SetCenter(double latitude, double longitude)
        {
            var latlng = new LatLng(latitude, longitude);
            mapsService.MoveFloatingOrigin(latlng, movedObjects);
            _signalBus.Fire<MapOriginChanged>();

            var coords = mapsService.Coords.FromLatLngToVector3(latlng);
            cameraControl.SetPosition(coords);
            mapsUpdater.LoadMap();
        }

        public void RegisterMovedObject(GameObject gameObject)
        {
            movedObjects.Add(gameObject);
        }
    }
}
