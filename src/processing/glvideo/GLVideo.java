/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Copyright (c) The Processing Foundation 2016
  Developed by Gottfried Haider

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.glvideo;

import java.io.File;
import processing.core.*;

/**
 *  @webref
 */
public class GLVideo {

  protected static boolean loaded = false;
  protected static boolean error = false;

  protected PApplet parent;
  protected long handle = 0;

  /**
   *  Datatype for playing video files, which can be located in the sketch's
   *  data folder, or on a remote URL. Since this library is using hardware
   *  accelerated video playback it is necessary to use it in combination with
   *  the P3D renderer.
   *  @param parent typically use "this"
   *  @param fn_or_uri filename or valid URL
   */
  public GLVideo(PApplet parent, String fn_or_uri) {
    super();
    this.parent = parent;

    if (!loaded) {
      System.loadLibrary("glvideo");
      loaded = true;
      if (gstreamer_init() == false) {
        error = true;
      }
    }

    if (error) {
      throw new RuntimeException("Could not load GStreamer");
    }

    if (fn_or_uri.indexOf("://") != -1) {
      // got URI, use as is
    } else {
      // get absolute path for fn
      // first, check Processing's dataPath
      File file = new File(parent.dataPath(fn_or_uri));
      if (file.exists() == false) {
        // next, the current directory
        file = new File(fn_or_uri);
      }
      if (file.exists()) {
        fn_or_uri = file.getAbsolutePath();
      }
    }

    handle = gstreamer_open(fn_or_uri);
    if (handle == 0) {
      throw new RuntimeException("Could not load video");
    }
  }

  /**
   *  Returns whether there is a new frame waiting to be displayed.
   */
  public boolean available() {
    if (handle == 0) {
      return false;
    } else {
      return gstreamer_isAvailable(handle);
    }
  }

  /**
   *  Get the most recent frame available as a GL texture.
   *  You can use the texture name (id) returned by this method until your
   *  next call of the getFrame method.
   *  @return texture name (id) to use with P3D
   */
  public int getFrame() {
    // TODO: alternative names: read(), getTexture()
    if (handle == 0) {
      return 0;
    } else {
      return gstreamer_getFrame(handle);
    }
  }

  /**
   *  Starts or resumes video playback.
   *  The play method will play a video file till the end and then stop.
   *  GLVideo objects start out paused, so you might want to call this
   *  method, or loop.
   */
  public void play() {
    if (handle != 0) {
      gstreamer_setLooping(handle, false);
      gstreamer_startPlayback(handle);
    }
  }

  /**
   *  Starts or resumes looping video playback.
   *  The loop method will continuously play back a video file.
   *  GLVideo objects start out paused, so you might want to call this
   *  method, or play.
   */
  public void loop() {
    if (handle != 0) {
      gstreamer_setLooping(handle, true);
      gstreamer_startPlayback(handle);
    }
  }

  /**
   *  Returns true if the video is playing or if playback got interrupted by buffering.
   */
  public boolean playing() {
    if (handle == 0) {
      return false;
    } else {
      return gstreamer_isPlaying(handle);
    }
  }

  /**
   *  Stop a looping video after the end of its current iteration.
   */
  public void noLoop() {
    if (handle != 0) {
      gstreamer_setLooping(handle, false);
    }
  }

  /**
   *  Jump to a specific time position in the video file.
   *  @param sec seconds from the start of the video
   */
  public void jump(float sec) {
    if (handle != 0) {
      if (!gstreamer_seek(handle, sec)) {
        System.err.println("Cannot jump to " + sec);
      }
    }
  }

  /**
   *  Change the speed in which a video file plays.
   *  Values larger than 1.0 will play the video faster than real time,
   *  while values lower than 1.0 will play it slower. Values lower than
   *  zero are currently not supported.
   *  @param rate playback rate (1.0 is real time)
   */
  public void speed(float rate) {
    if (handle != 0) {
      if (!gstreamer_setSpeed(handle, rate)) {
        System.err.println("Cannot set speed to to " + rate);
      }
    }
  }

  /**
   *  Pause a video file.
   *  Playback can be resumed with the play or loop methods.
   */
  public void pause() {
    if (handle != 0) {
      gstreamer_stopPlayback(handle);
    }
  }

  /**
   *  Return the total length of the movie file in seconds.
   */
  public float duration() {
    if (handle == 0) {
      return 0.0f;
    } else {
      return gstreamer_getDuration(handle);
    }
  }

  /**
   *  Return the current time position in seconds.
   */
  public float time() {
    if (handle == 0) {
      return 0.0f;
    } else {
      return gstreamer_getPosition(handle);
    }
  }

  /**
   *  Return the native width of the movie file in pixels.
   */
  public int width() {
    if (handle == 0) {
      return 0;
    } else {
      return gstreamer_getWidth(handle);
    }
  }

  /**
   *  Return the native height of the movie file in pixels.
   */
  public int height() {
    if (handle == 0) {
      return 0;
    } else {
      return gstreamer_getHeight(handle);
    }
  }

  /**
   *  Return the native frame rate of the movie file in frames per second (fps).
   *  This is currently not implemented.
   */
  public float frameRate() {
    if (handle == 0) {
      return 0.0f;
    } else {
      return gstreamer_getFramerate(handle);
    }
  }

  /**
   *  Close a movie file.
   *  This method releases all resources associated with the playback of a movie file.
   *  Call close before loading and playing back a second file.
   */
  public void close() {
    if (handle != 0) {
      gstreamer_close(handle);
      handle = 0;
    }
  }


  private static native boolean gstreamer_init();
  private native long gstreamer_open(String fn_or_uri);
  private native boolean gstreamer_isAvailable(long handle);
  private native int gstreamer_getFrame(long handle);
  private native void gstreamer_startPlayback(long handle);
  private native boolean gstreamer_isPlaying(long handle);
  private native void gstreamer_stopPlayback(long handle);
  private native void gstreamer_setLooping(long handle, boolean looping);
  private native boolean gstreamer_seek(long handle, float sec);
  private native boolean gstreamer_setSpeed(long handle, float rate);
  private native boolean gstreamer_setVolume(long handle, float vol);
  private native float gstreamer_getDuration(long handle);
  private native float gstreamer_getPosition(long handle);
  private native int gstreamer_getWidth(long handle);
  private native int gstreamer_getHeight(long handle);
  private native float gstreamer_getFramerate(long handle);
  private native void gstreamer_close(long handle);
}
