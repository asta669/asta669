"""Generate the three original Wake Up alarm loops (standard library only).

No sampled or third-party sounds. Short note envelopes prevent digital clicks;
there is no gradual alarm fade-in. Output is mono PCM16 at 44.1 kHz.
"""
import math
from pathlib import Path
import struct
import wave

RATE = 44100
DURATION = 4
DEST = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'


def tone(t, frequency, length):
    attack = min(t / .008, 1.0)
    release = min(max((length - t) / .018, 0), 1.0)
    envelope = max(0, min(attack, release))
    fundamental = math.sin(2 * math.pi * frequency * t)
    harmonic = .22 * math.sin(2 * math.pi * frequency * 2 * t)
    return envelope * (fundamental + harmonic) / 1.22


def sample(name, t):
    if name == 'pulse':
        offset = t % .5
        return tone(offset, 784 if int(t * 2) % 2 == 0 else 988, .32) if offset < .32 else 0
    if name == 'beacon':
        offset = t % 1
        if offset < .22:
            return tone(offset, 660, .22)
        if .3 <= offset < .62:
            return tone(offset - .3, 880, .32)
        return 0
    offset = t % .25
    frequency = (587.33, 739.99, 880, 1174.66)[int(t * 4) % 4]
    return tone(offset, frequency, .21) if offset < .21 else 0


if __name__ == '__main__':
    DEST.mkdir(parents=True, exist_ok=True)
    for name in ('pulse', 'beacon', 'rise'):
        target = DEST / f'alarm_{name}.wav'
        with wave.open(str(target), 'wb') as output:
            output.setnchannels(1)
            output.setsampwidth(2)
            output.setframerate(RATE)
            output.writeframes(b''.join(struct.pack('<h', round(sample(name, i / RATE) * .92 * 32767))
                                        for i in range(RATE * DURATION)))
        print(target.name)
