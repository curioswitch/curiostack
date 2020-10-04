using System;
using System.Collections.Generic;
using CafeMap.Player.Services;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class FixedPositionModels : MonoBehaviour
    {
        [Serializable]
        public class PositionedModel
        {
            public double Latitude;
            public double Longitude;
            public GameObject Model;
            public float Scale;
            public float Rotation;
        }
        
        [SerializeField]
        private List<PositionedModel> models;

        private MapsService mapsService;
        private ViewportService viewportService;
        
        public IEnumerable<PositionedModel> Models
        {
            get { return models; }
            set
            {
                models.Clear();
                models.AddRange(value);
            }
        }

        [Inject]
        public void Init(MapsService mapsService, ViewportService viewportService)
        {
            this.mapsService = mapsService;
            this.viewportService = viewportService;
        }

        private void Start()
        {
            foreach (var model in models)
            {
                var instantiated = Instantiate(model.Model);
                var position = mapsService.Coords.FromLatLngToVector3(new LatLng(model.Latitude, model.Longitude));
                instantiated.transform.position = position;
                instantiated.transform.Rotate(Vector3.up, model.Rotation);
                instantiated.transform.localScale = new Vector3(model.Scale, model.Scale, model.Scale);
                viewportService.RegisterMovedObject(instantiated);
            }
        }
    }
}