import os, sys
from setuptools import find_packages, setup
from albus import __version__

if sys.version_info < (2,6):
    raise NotImplementedError("albus requires Python 2.6 or later")

setup(
    name = "albus",
    version = __version__,
    description = "Albus RPC Library",
    long_description = file(
        os.path.join(
            os.path.dirname(__file__),
            'README.rst'
        )
    ).read(),

    classifiers = [
        'Development Status :: 4 - Beta',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: BSD License',
        'Operating System :: OS Independent',
        'Programming Language :: Python',
        'Topic :: Software Development :: Object Brokering',
        'Topic :: Software Development :: Libraries',
    ],

    url = "http://project.hejinyun.com",

    author = "Adoal Xu",
    author_email = "adoalxu@gmail.com",
    license = "BSD",

    platforms = "any",
    packages = find_packages(exclude=["test"]),
    zip_safe = True,
)

