using System;
using CafeMap.Events;
using CafeMap.Player.Services;
using Org.Curioswitch.Cafemap.Api;
using UnityEngine;
using UnityEngine.EventSystems;
using Zenject;

namespace CafeMap.Map
{
    public class PlaceResult : MonoBehaviour, IPointerClickHandler
    {

        private ViewportService viewportService;
        private SignalBus _signalBus;

        public Place place;

        [Inject]
        public void Init(ViewportService viewportService, SignalBus signalBus)
        {
            this.viewportService = viewportService;
            _signalBus = signalBus;
        }

        public void OnPointerClick(PointerEventData ignored)
        {
            SelectResult();
        }

        public void SelectResult()
        {
            _signalBus.Fire(PlaceSelected.create(place));
        }
    }
}
