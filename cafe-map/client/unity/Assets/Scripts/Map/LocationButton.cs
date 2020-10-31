using System.Collections;
using System.Collections.Generic;
using CafeMap.Player.Services;
using UnityEngine;
using UnityEngine.EventSystems;
using Zenject;

public class LocationButton : MonoBehaviour, IPointerClickHandler
{

    private ViewportService _viewportService;

    [Inject]
    public void Init(ViewportService viewportService)
    {
        _viewportService = viewportService;
    }

    public void OnPointerClick(PointerEventData eventData)
    {
        var location = CafeMap.Player.LocationService.getLocation();
        _viewportService.SetCenter(location.Lat, location.Lng);
    }
}
