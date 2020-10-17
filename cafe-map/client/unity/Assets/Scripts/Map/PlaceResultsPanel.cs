using System;
using System.Collections.Generic;
using System.Linq;
using CafeMap.Services;
using Cysharp.Threading.Tasks;
using ModestTree;
using Org.Curioswitch.Cafemap.Api;
using UniRx;
using UnityEngine;
using UnityEngine.UI;
using Zenject;

namespace CafeMap.Map
{
    public class PlaceResultsPanel : MonoBehaviour
    {
        private readonly Dictionary<string, Place> visiblePlaces = new Dictionary<string, Place>();
        private readonly Subject<bool> visiblePlacesChanged = new Subject<bool>();

        private GameObject imageHolderPrefab;

        private PlacesService _placesService;

        [Inject]
        public void Init(PlacesService placesService)
        {
            _placesService = placesService;
        }

        private void Awake()
        {
            imageHolderPrefab = Resources.Load<GameObject>("Prefabs/UI/ImageHolder");

            visiblePlacesChanged.AsObservable()
                .Throttle(TimeSpan.FromMilliseconds(500))
                .Select(ignored => visiblePlaces.Values.ToList())
                .Subscribe(rerender);

            for (int i = 0; i < 20; i++)
            {
                addResultPlaceholder();
            }

            transform.parent.gameObject.SetActive(false);
        }

        public void addVisiblePlace(Place place)
        {
            visiblePlaces[place.Id] = place;
            visiblePlacesChanged.OnNext(true);
        }

        public void removeVisiblePlace(Place place)
        {
            visiblePlaces.Remove(place.Id);
            visiblePlacesChanged.OnNext(true);
        }

        private async void rerender(List<Place> places)
        {
            Debug.Log("Rerendering place results");
            var images = await UniTask.WhenAll(
                places
                    .Where(place => !place.GooglePlaceId.IsEmpty())
                    .Select(place => _placesService.getPhoto(place.GooglePlaceId)));

            if (images.IsEmpty())
            {
                transform.parent.gameObject.SetActive(false);
                return;
            }

            transform.parent.gameObject.SetActive(true);

            int numMissingPlaceholders = images.Length - transform.childCount;
            for (int i = 0; i < numMissingPlaceholders; i++)
            {
                addResultPlaceholder();
            }

            for (int i = 0; i < images.Length; i++)
            {
                var place = places[i];
                var sprite = images[i];
                var imageObject = transform.GetChild(i).gameObject;
                imageObject.SetActive(true);
                imageObject.name = "Image " + place.Name;
                var image = imageObject.transform.GetChild(0).gameObject.GetComponent<Image>();

                float spriteScale = 1.0f;
                if (sprite.texture.width < 400)
                {
                    spriteScale = 400.0f / sprite.texture.width;
                }

                if (sprite.texture.height < 400)
                {
                    spriteScale = Math.Max(spriteScale, 400.0f / sprite.texture.height);
                }

                image.preserveAspect = true;
                image.sprite = sprite;
                image.rectTransform.sizeDelta =
                    new Vector2(spriteScale * sprite.texture.width, spriteScale * sprite.texture.height);
            }

            for (int i = images.Length; i < transform.childCount; i++)
            {
                transform.GetChild(i).gameObject.SetActive(false);
            }
        }

        private void addResultPlaceholder()
        {
            var imageObject = Instantiate(imageHolderPrefab, transform);
            imageObject.SetActive(false);
        }
    }
}
