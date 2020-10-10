using System;
using System.Collections.Generic;
using CafeMap.Services;
using GoogleApi;
using GoogleApi.Entities.Places.Photos.Request;
using GoogleApi.Entities.Places.Photos.Response;
using UnityEngine;
using UnityEngine.UI;
using Zenject;

namespace CafeMap.Map
{
    public class PlaceResultsPanel : MonoBehaviour
    {
        private readonly HashSet<Org.Curioswitch.Cafemap.Api.Place> visiblePlaces = new HashSet<Org.Curioswitch.Cafemap.Api.Place>();

        private PlacesService _placesService;

        private RectTransform _rectTransform;

        [Inject]
        public void Init(PlacesService placesService)
        {
            _placesService = placesService;
        }

        private void Awake()
        {
            _rectTransform = GetComponent<RectTransform>();
        }

        public void addVisiblePlace(Org.Curioswitch.Cafemap.Api.Place place)
        {
            if (visiblePlaces.Add(place))
            {
                rerender();
            }
        }

        public void removeVisiblePlace(Org.Curioswitch.Cafemap.Api.Place place)
        {
            if (visiblePlaces.Remove(place))
            {
                rerender();
            }
        }

        private async void rerender()
        {
            var visiblePlaces = new List<Org.Curioswitch.Cafemap.Api.Place>(this.visiblePlaces);
            List<Texture2D> images = new List<Texture2D>();
            for (int i = 0; i < 20; i++)
            {
                images.Add(await _placesService.getPhoto("ChIJBdHOjD9dGGARk142RKVZsz0"));
            }

            foreach (Transform child in transform)
            {
                Destroy(child.gameObject);
            }

            for (int i = 0; i < images.Count; i++)
            {
                // var place = visiblePlaces[i];
                var texture = images[i];
                var imageObject = new GameObject("Image");
                imageObject.transform.SetParent(transform);
                var image = imageObject.AddComponent<Image>();
                image.sprite = Sprite.Create(texture, new Rect(0, 0, texture.width, texture.height), Vector2.zero);
                image.rectTransform.sizeDelta = new Vector2(400, 400);
                image.preserveAspect = true;
            }
        }
    }
}
