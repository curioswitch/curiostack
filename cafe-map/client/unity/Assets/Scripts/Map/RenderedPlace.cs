using CafeMap.Player.Services;
using Extensions.Runtime;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class RenderedPlace : MonoBehaviour
    {
        public Org.Curioswitch.Cafemap.Api.Place Place;

        private PlaceResultsPanel _placeResultsPanel;
        private ViewportService _viewportService;

        private Camera _camera;
        private RectTransform _rectTransform;

        private bool _visible;

        [Inject]
        public void Init(PlaceResultsPanel placeResultsPanel, ViewportService viewportService)
        {
            _placeResultsPanel = placeResultsPanel;
            _viewportService = viewportService;
        }

        private void Awake()
        {
            _rectTransform = GetComponent<RectTransform>();
        }

        private void Start()
        {
            _camera = Camera.main;
            _visible = _rectTransform.IsFullyVisibleFrom(_camera);
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
            _viewportService.SetCenter(Place.Position.Longitude, Place.Position.Longitude);
        }
    }
}
