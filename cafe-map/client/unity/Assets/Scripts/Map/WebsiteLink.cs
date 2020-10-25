using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;

public class WebsiteLink : MonoBehaviour, IPointerClickHandler
{

    private string url;

    public void SetUrl(string url)
    {
        this.url = url;
    }

    public void OnPointerClick(PointerEventData eventData)
    {
        Application.OpenURL(url);
    }
}
