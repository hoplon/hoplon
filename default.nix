let
 pkgs = import <nixpkgs> { };
in
{
 core-shell = pkgs.stdenv.mkDerivation {
  name = "hoplon-shell";

  buildInputs = [
   pkgs.boot
   pkgs.phantomjs2
  ]
  ;
 };
}
