using CafeMap.Events;
using CafeMap.Player.Services;
using Extensions.Runtime;
using UnityEngine;
using UnityEngine.EventSystems;
using Zenject;

namespace CafeMap.Map
{
    public class PlacePin : MonoBehaviour, IPointerClickHandler
    {
        private static readonly Vector3 NORMAL_SIZE = new Vector3(0.2f, 0.2f, 0.2f);
        private static readonly Vector3 LARGE_SIZE = new Vector3(0.4f, 0.4f, 0.4f);

        public Org.Curioswitch.Cafemap.Api.Place Place;

        private PlaceResultsPanel _placeResultsPanel;
        private ViewportService _viewportService;
        private SignalBus _signalBus;

        private Camera _camera;
        private RectTransform _rectTransform;

        private bool _visible;

        [Inject]
        public void Init(PlaceResultsPanel placeResultsPanel, ViewportService viewportService, SignalBus signalBus)
        {
            _placeResultsPanel = placeResultsPanel;
            _viewportService = viewportService;
            _signalBus = signalBus;
        }

        private void Awake()
        {
            _rectTransform = GetComponent<RectTransform>();
        }

        private void Start()
        {
            _camera = Camera.main;
            _visible = _rectTransform.IsFullyVisibleFrom(_camera);

            _signalBus.Subscribe<PlaceSelected>(onPlaceSelected);
        }

        private void Update()
        {
            // var cameraEuler = Camera.main.transform.eulerAngles;
            transform.rotation = _camera.transform.rotation;

            if (_visible && !_rectTransform.IsFullyVisibleFrom(_camera))
            {
                _visible = false;
                _placeResultsPanel.removeVisiblePlace(Place);
            } else if (!_visible && _rectTransform.IsFullyVisibleFrom(_camera))
            {
                _visible = true;
                _placeResultsPanel.addVisiblePlace(Place);
            }
        }

        private void OnBecameVisible()
        {
            _placeResultsPanel.addVisiblePlace(Place);
        }

        private void OnBecameInvisible()
        {
            _placeResultsPanel.removeVisiblePlace(Place);
        }

        public void Select()
        {
            _signalBus.Fire(PlaceSelected.create(Place));
        }

        public void OnPointerClick(PointerEventData eventData)
        {
            Select();
        }

        private void onPlaceSelected(PlaceSelected selected)
        {
            transform.localScale = selected.Place.GooglePlaceId == Place.GooglePlaceId ? LARGE_SIZE : NORMAL_SIZE;
        }
    }
}
