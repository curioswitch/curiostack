using System;
using UnityEngine;
using Zenject;

namespace CafeMap.Map
{
    public class RenderedPlace : MonoBehaviour
    {
        public Org.Curioswitch.Cafemap.Api.Place Place;

        private PlaceResultsPanel _placeResultsPanel;

        [Inject]
        public void Init(PlaceResultsPanel placeResultsPanel)
        {
            _placeResultsPanel = placeResultsPanel;
        }

        private void OnBecameVisible()
        {
            _placeResultsPanel.addVisiblePlace(Place);
        }

        private void OnBecameInvisible()
        {
            _placeResultsPanel.removeVisiblePlace(Place);
        }
    }
}
