using System;
using CafeMap.Player.Services;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class RenderedPlace : MonoBehaviour
    {
        public Org.Curioswitch.Cafemap.Api.Place Place;

        private PlaceResultsPanel _placeResultsPanel;
        private ViewportService _viewportService;

        [Inject]
        public void Init(PlaceResultsPanel placeResultsPanel, ViewportService viewportService)
        {
            _placeResultsPanel = placeResultsPanel;
            _viewportService = viewportService;
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
