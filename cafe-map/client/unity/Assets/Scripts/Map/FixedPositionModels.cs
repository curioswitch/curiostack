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
    public class FixedPositionModels : MonoBehaviour, IInitializable
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
        private DiContainer container;

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
        public void Init(MapsService mapsService, ViewportService viewportService, DiContainer container, SignalBus signalBus)
        {
            this.mapsService = mapsService;
            this.viewportService = viewportService;
            this.container = container;

            signalBus.Subscribe<MapOriginChanged>(recomputeBounds);
        }

        public void Initialize()
        {
            foreach (var model in models)
            {
                var instantiated = Instantiate(model.Model);
                var positionable = container.InstantiateComponent<WorldPositionable>(instantiated);
                positionable.Latitude = model.Latitude;
                positionable.Longitude = model.Longitude;
                positionable.Rotation = model.Rotation;
                positionable.Scale = model.Scale;
                renderedModels.Add(instantiated);
            }
        }

        private void Start()
        {
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
