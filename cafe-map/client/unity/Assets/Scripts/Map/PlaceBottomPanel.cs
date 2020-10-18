using System;
using CafeMap.Events;
using CafeMap.Player.Services;
using CafeMap.Services;
using Cysharp.Threading.Tasks;
using Org.Curioswitch.Cafemap.Api;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using Zenject;

namespace CafeMap.Map
{
    public class PlaceBottomPanel : MonoBehaviour, IPointerClickHandler
    {

        private static readonly Color UNSELECTED_COLOR = new Color(1, 1, 1, 0.5f);
        private static readonly Color SELECTED_COLOR = new Color(1, 0, 1, 0.5f);

        private PlacesService _placesService;
        private ViewportService viewportService;
        private SignalBus _signalBus;

        private bool loadedPlaceImage;

        private Image placeImage;
        private Text panelText;
        private Image panelImage;

        private Place _place;

        [Inject]
        public void Init(PlacesService placesService, ViewportService viewportService, SignalBus signalBus, Place place)
        {
            _placesService = placesService;
            this.viewportService = viewportService;
            _signalBus = signalBus;
            _place = place;
        }

        private void Awake()
        {
            _signalBus.Subscribe<PlaceSelected>(ONPlaceSelected);
            placeImage = transform.GetChild(0).GetComponent<Image>();
            panelImage = transform.GetChild(1).GetComponent<Image>();
            panelText = transform.GetChild(1).GetChild(0).GetComponent<Text>();
            panelImage.color = UNSELECTED_COLOR;
        }

        private void Start()
        {
            panelText.text = _place.Name;
        }

        public async UniTask Activate()
        {
            if (!loadedPlaceImage)
            {
                await fetchPhoto();
                gameObject.SetActive(true);
            }
            else
            {
                gameObject.SetActive(true);
            }
        }

        public void Deactivate()
        {
            gameObject.SetActive(false);
        }

        public void OnPointerClick(PointerEventData ignored)
        {
            SelectResult();
        }

        public void SelectResult()
        {
            _signalBus.Fire(PlaceSelected.create(_place));
        }

        private void ONPlaceSelected(PlaceSelected selected)
        {
            if (_place.Id == selected.Place.Id)
            {
                panelImage.color = SELECTED_COLOR;
            }
            else
            {
                panelImage.color = UNSELECTED_COLOR;
            }
        }

        private async UniTask fetchPhoto()
        {
            var sprite = await _placesService.getPhoto(_place.GooglePlaceId);
            if (sprite == null)
            {
                return;
            }

            float spriteScale = 1.0f;
            if (sprite.texture.width < 400)
            {
                spriteScale = 400.0f / sprite.texture.width;
            }

            if (sprite.texture.height < 400)
            {
                spriteScale = Math.Max(spriteScale, 400.0f / sprite.texture.height);
            }

            placeImage.preserveAspect = true;
            placeImage.sprite = sprite;
            placeImage.rectTransform.sizeDelta = new Vector2(spriteScale * sprite.texture.width, spriteScale * sprite.texture.height);

            loadedPlaceImage = true;
        }
    }
}
