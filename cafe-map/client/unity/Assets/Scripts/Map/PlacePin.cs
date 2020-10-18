using CafeMap.Events;
using CafeMap.Player.Services;
using Extensions.Runtime;
using Org.Curioswitch.Cafemap.Api;
using UnityEngine;
using UnityEngine.EventSystems;
using Zenject;

namespace CafeMap.Map
{
    public class PlacePin : MonoBehaviour, IPointerClickHandler
    {
        private static readonly Vector3 NORMAL_SIZE = new Vector3(0.2f, 0.2f, 0.2f);
        private static readonly Vector3 LARGE_SIZE = new Vector3(0.4f, 0.4f, 0.4f);

        private Place _place;

        private PlacesRenderer _placesRenderer;
        private ViewportService _viewportService;
        private SignalBus _signalBus;

        private Camera _camera;
        private RectTransform _rectTransform;

        private bool _visible;

        [Inject]
        public void Init(PlacesRenderer placesRenderer, ViewportService viewportService, SignalBus signalBus, Place place)
        {
            _placesRenderer = placesRenderer;
            _viewportService = viewportService;
            _signalBus = signalBus;
            _place = place;
        }

        private void Awake()
        {
            _rectTransform = GetComponent<RectTransform>();
        }

        private void Start()
        {
            _camera = Camera.main;
            _visible = _rectTransform.IsFullyVisibleFrom(_camera);

            _signalBus.Subscribe<PlaceSelected>(ONPlaceSelected);
        }

        private void Update()
        {
            // var cameraEuler = Camera.main.transform.eulerAngles;
            transform.rotation = _camera.transform.rotation;

            if (_visible && !_rectTransform.IsVisibleFrom(_camera))
            {
                _visible = false;
                _placesRenderer.SetInivisiblePlace(_place);
            } else if (!_visible && _rectTransform.IsVisibleFrom(_camera))
            {
                _visible = true;
                _placesRenderer.SetVisiblePlace(_place);
            }
        }

        public void Select()
        {
            _signalBus.Fire(PlaceSelected.create(_place));
        }

        public void OnPointerClick(PointerEventData eventData)
        {
            Select();
        }

        private void ONPlaceSelected(PlaceSelected selected)
        {
            transform.localScale = selected.Place.GooglePlaceId == _place.GooglePlaceId ? LARGE_SIZE : NORMAL_SIZE;
        }
    }
}
