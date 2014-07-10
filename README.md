unused-image-finder
===================

Finds unused images in a FrameMaker project folder.

What the program does:
 - Lets FrameMaker users quickly find all images in their project folder that are
no longer used and can be deleted from their computer and/or source control.
(Right now my team does this using an annoying combination of a batch file and
a slow Excel file.)

How it does it:
- It assumes you have a folder with all of a book's images in it, and that this
folder is directly inside the project folder.
- You specify the folder that has all the images in it.
- You paste in a FrameMaker-generated list of exported graphic references (i.e.,
the list of used images). Each line in this list will look like this:
"Graphics/screenshot.png @ 120 dpi 100" [minus the quotes]
- When you click Show Unused Images, a new text area appears that shows all the
images in the specified image folder that are NOT mentioned in the list of
used images.

How you can test it:
- Select any non-empty folder on your machine (e.g., "YourName/Home/").
- Click Show Unused Images. All the regular files in your specified folder will
appear (e.g., "MyFile.txt").
- Type something like "Home/MyFile.txt @" in the FrameMaker's List area.
- Click Show Unused Images again. The file should disappear from the Used Images
area.
