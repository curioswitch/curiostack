using System;
using System.Collections.Generic;
using System.Linq;
using CafeMap.Player.Services;
using Cysharp.Threading.Tasks;
using Google.Maps;
using ModestTree;
using Org.Curioswitch.Cafemap.Api;
using UniRx;
using UnityEngine;
using Zenject;
using LatLng = Google.Maps.Coord.LatLng;

namespace CafeMap.Map
{
    public class PlacesRenderer : MonoBehaviour
    {
        [SerializeField]
        private GameObject pinPrefab;

        [SerializeField]
        private GameObject resultPanelPrefab;

        [SerializeField] private GameObject resultsPanel;

        [SerializeField] private GameObject resultsContainer;

        private readonly HashSet<string> visiblePlaces = new HashSet<string>();
        private readonly Subject<bool> visiblePlacesChanged = new Subject<bool>();

        private readonly Dictionary<string, PlacePin> placePins = new Dictionary<string, PlacePin>();
        private readonly Dictionary<string, PlaceBottomPanel> placeResultPanels = new Dictionary<string, PlaceBottomPanel>();

        private SecretsService secretsService;
        private MapsService mapsService;
        private Canvas worldCanvas;
        private DiContainer _container;

        [Inject]
        public void Init(SecretsService secretsService, MapsService mapsService, Canvas worldCanvas, DiContainer container)
        {
            this.secretsService = secretsService;
            this.mapsService = mapsService;
            this.worldCanvas = worldCanvas;
            _container = container;
        }

        private void Awake()
        {
            visiblePlacesChanged.AsObservable()
                .Throttle(TimeSpan.FromMilliseconds(500))
                .Subscribe(rerender);
        }

        public void SetVisiblePlace(Place place)
        {
            visiblePlaces.Add(place.Id);
            visiblePlacesChanged.OnNext(true);
        }

        public void SetInvisiblePlace(Place place)
        {
            visiblePlaces.Remove(place.Id);
            visiblePlacesChanged.OnNext(true);
        }

        private void Start()
        {
            foreach (var place in secretsService.PlaceDb.Place
                .Where(place => !place.GooglePlaceId.IsEmpty())
                .GroupBy(p => p.GooglePlaceId)
                .Select(grp => grp.FirstOrDefault()))
            {
                var pin = _container.InstantiatePrefabForComponent<PlacePin>(pinPrefab, worldCanvas.gameObject.transform,
                    new object[] {place});
                pin.gameObject.name = place.Name + " (Pin)";

                var latLng = new LatLng(place.Position.Latitude, place.Position.Longitude);
                var position = mapsService.Coords.FromLatLngToVector3(latLng);
                position.y = 10;
                pin.transform.position = position;
                placePins[place.Id] = pin;

                var panel = _container.InstantiatePrefabForComponent<PlaceBottomPanel>(resultPanelPrefab, resultsPanel.transform, new object[] { place });
                var panelObject = panel.gameObject;
                panelObject.transform.rotation = Quaternion.identity;
                panelObject.SetActive(false);
                panelObject.name = place.Name + " (Result Panel)";
                placeResultPanels[place.Id] = panel;
            }

            resultsContainer.SetActive(false);
        }


        private void rerender(bool ignored)
        {
            Debug.Log("Rerendering: " + visiblePlaces.Count);
            if (visiblePlaces.IsEmpty())
            {
                resultsContainer.SetActive(false);
                return;
            }
            resultsContainer.SetActive(true);
            foreach (var placePanel in placeResultPanels)
            {
                if (visiblePlaces.Contains(placePanel.Key))
                {
                    placePanel.Value.Activate();
                }
                else
                {
                    placePanel.Value.Deactivate();
                }
            }
        }
    }
}
