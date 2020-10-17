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

        public Place place;

        [Inject]
        public void Init(ViewportService viewportService)
        {
            this.viewportService = viewportService;
        }

        public void OnPointerClick(PointerEventData ignored)
        {
            SelectResult();
        }

        public void SelectResult()
        {
            viewportService.SetCenter(place.Position.Latitude, place.Position.Longitude);
        }
    }
}
