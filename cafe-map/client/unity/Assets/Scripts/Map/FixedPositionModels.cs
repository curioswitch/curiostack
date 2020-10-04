using System;
using System.Collections.Generic;
using CafeMap.Events;
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

        private readonly List<GameObject> renderedModels = new List<GameObject>();
        private readonly List<Bounds> renderedBounds = new List<Bounds>();

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
        public void Init(MapsService mapsService, ViewportService viewportService, SignalBus signalBus)
        {
            this.mapsService = mapsService;
            this.viewportService = viewportService;

            signalBus.Subscribe<MapOriginChanged>(recomputeBounds);
        }

        private void Start()
        {
            foreach (var model in models)
            {
                var instantiated = Instantiate(model.Model);
                var position = mapsService.Coords.FromLatLngToVector3(new LatLng(model.Latitude, model.Longitude));
                instantiated.transform.position = position;
                instantiated.transform.Rotate(Vector3.up, model.Rotation);
                instantiated.transform.Translate(0, -100, 0);
                instantiated.transform.localScale = new Vector3(model.Scale, model.Scale, model.Scale);
                viewportService.RegisterMovedObject(instantiated);
                renderedModels.Add(instantiated);
            }

            recomputeBounds();

            mapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(args =>
            {
                foreach (var bounds in renderedBounds)
                {
                    if (bounds.Intersects(args.MapFeature.Shape.BoundingBox))
                    {
                        args.Cancel = true;
                        break;
                    }
                }
            });
        }

        private void recomputeBounds()
        {
            renderedBounds.Clear();
            foreach (var obj in renderedModels)
            {
                Bounds bounds = new Bounds();
                foreach (var renderer in obj.GetComponentsInChildren<Renderer>())
                {
                    bounds.Encapsulate(renderer.bounds);
                }
                renderedBounds.Add(bounds);
            }
        }
    }
}
