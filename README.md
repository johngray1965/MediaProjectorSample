# MediaProjectorSample
A Sample Android MediaProjector project to help sort out memory leak

I *think* I'm seeing a memory leak in the Media Projector.  Maybe its just in my use of it.

To see the issue, start an emulator that's API 21 or greater.

Run ./start_malloc_debug

After the emulator restart, run the app.

Start the recording with the FAB.

Scroll to the end of the list and back.

Stop recording.

Run ./get_sample_heap_dump

get_sample_heap_dump will copy the heap dump to nhp (it'll overwrite if there's aleady one there).  Its a text file, you see the 4 Mb buffers at the top of the lsit.

Repeat from starting recording.   After the first few iterations You'll see the 4Mb buffers accumulate.  After a few iterations I see 9 of them (it may grow further, I'm not sure).
